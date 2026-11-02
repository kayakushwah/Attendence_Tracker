import 'dart:typed_data';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:vibration/vibration.dart';

class AlertManager {
  static final FlutterLocalNotificationsPlugin _notificationsPlugin = FlutterLocalNotificationsPlugin();
  
  static Future<void> initialize() async {
    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings();
    const initSettings = InitializationSettings(android: androidInit, iOS: iosInit);
    
    await _notificationsPlugin.initialize(initSettings);
  }

  static Future<void> fireAttendanceAlert(String matchedKeyword, double confidence) async {
    // Vibrate urgently
    final hasVibrator = await Vibration.hasVibrator();
    if (hasVibrator ?? false) {
      Vibration.vibrate(pattern: [0, 500, 200, 500, 200, 500, 200, 1000]);
    }

    // High priority notification
    final percent = (confidence * 100).toInt();
    
    const androidDetails = AndroidNotificationDetails(
      'alert_channel',
      'Attendance Alert',
      channelDescription: 'Alerts you when your name or roll number is called',
      importance: Importance.max,
      priority: Priority.high,
      enableVibration: true,
      playSound: true,
      fullScreenIntent: true,
      vibrationPattern: Int64List.fromList([0, 500, 200, 500, 200, 500]),
    );
    const iosDetails = DarwinNotificationDetails();
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);

    await _notificationsPlugin.show(
      7500,
      '🚨 YOUR ATTENDANCE! 🚨',
      '"$matchedKeyword" was called! ($percent% match)',
      details,
    );
  }
}
