import 'dart:async';
import 'dart:ui';
import 'package:flutter_background_service/flutter_background_service.dart';
import 'package:speech_to_text/speech_to_text.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'name_detector.dart';
import 'alert_manager.dart';

Future<void> initializeService() async {
  final service = FlutterBackgroundService();
  await AlertManager.initialize();

  await service.configure(
    androidConfiguration: AndroidConfiguration(
      onStart: onStart,
      autoStart: false,
      isForegroundMode: true,
      notificationChannelId: 'alert_channel',
      initialNotificationTitle: 'rollcall',
      initialNotificationContent: 'vibing in background...',
      foregroundServiceNotificationId: 7501,
    ),
    iosConfiguration: IosConfiguration(
      autoStart: false,
      onForeground: onStart,
      onBackground: onIosBackground,
    ),
  );
}

@pragma('vm:entry-point')
Future<bool> onIosBackground(ServiceInstance service) async {
  return true;
}

@pragma('vm:entry-point')
void onStart(ServiceInstance service) async {
  DartPluginRegistrant.ensureInitialized();
  
  final prefs = await SharedPreferences.getInstance();
  final nameDetector = NameDetector();
  
  final userName = prefs.getString('userName') ?? '';
  final rollNumber = prefs.getString('rollNumber') ?? '';
  final extraKeywords = prefs.getString('extraKeywords') ?? '';
  final sensitivity = prefs.getString('sensitivity') ?? 'medium';

  nameDetector.setKeywords(userName, rollNumber, extraKeywords);
  nameDetector.setSensitivity(sensitivity);

  if (service is AndroidServiceInstance) {
    service.on('setAsForeground').listen((event) {
      service.setAsForegroundService();
    });
    service.on('setAsBackground').listen((event) {
      service.setAsBackgroundService();
    });
  }

  service.on('stopService').listen((event) {
    service.stopSelf();
  });

  final stt = SpeechToText();
  bool isAlertCooldown = false;
  
  bool available = await stt.initialize(
    onError: (val) {
      print('Speech Error: ${val.errorMsg}');
      Future.delayed(const Duration(seconds: 2), () {
        if (!isAlertCooldown) _startListeningLoop(service, stt, nameDetector, () => isAlertCooldown = true);
      });
    },
    onStatus: (val) {
      print('Speech Status: $val');
      if (val == 'done' || val == 'notListening') {
         if (!isAlertCooldown) _startListeningLoop(service, stt, nameDetector, () => isAlertCooldown = true);
      }
    },
  );

  if (available) {
    _startListeningLoop(service, stt, nameDetector, () => isAlertCooldown = true);
  }
}

void _startListeningLoop(ServiceInstance service, SpeechToText stt, NameDetector nameDetector, Function triggerCooldown) {
  if (stt.isListening) return;

  stt.listen(
    onResult: (val) {
      final result = nameDetector.analyze(val.recognizedWords);
      if (result.matched) {
        AlertManager.fireAttendanceAlert(result.matchedKeyword, result.confidence);
        service.invoke('matchFound', {'keyword': result.matchedKeyword});
        triggerCooldown();
        stt.stop();
        
        Future.delayed(const Duration(seconds: 15), () {
            _startListeningLoop(service, stt, nameDetector, triggerCooldown);
        });
      }
    },
    listenFor: const Duration(seconds: 30),
    pauseFor: const Duration(seconds: 3),
    partialResults: true,
  );
}
