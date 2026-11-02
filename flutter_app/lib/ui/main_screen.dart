import 'dart:math' as math;
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:lottie/lottie.dart';
import 'package:flutter_background_service/flutter_background_service.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'profile_setup_screen.dart';

enum AppState { idle, listening, alerting }

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});
  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> with TickerProviderStateMixin {
  AppState currentState = AppState.idle;
  String rollNumber = '...';
  String matchedWord = '';
  
  late AnimationController _waveController;
  late AnimationController _floatingController;

  // Memes State
  String? currentMemeUrl;
  String? currentMemeText;
  bool isFetchingMeme = false;

  final List<String> sleepingCatLotties = [
    'https://assets2.lottiefiles.com/packages/lf20_syqnfe7c.json',
    'https://assets9.lottiefiles.com/packages/lf20_lc46h4rm.json',
  ];
  final List<String> vibingCatLotties = [
    'https://assets1.lottiefiles.com/packages/lf20_6aYlBl.json',
    'https://assets10.lottiefiles.com/packages/lf20_t24pxtxz.json',
  ];

  @override
  void initState() {
    super.initState();
    _loadProfile();
    _fetchRandomMeme();
    
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);

    _floatingController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat(reverse: true);

    FlutterBackgroundService().isRunning().then((running) {
       if (running && mounted) {
          setState(() => currentState = AppState.listening);
       }
    });

    FlutterBackgroundService().on('update').listen((event) {
      if (mounted && event != null && event['is_listening'] == true) {
         if (currentState == AppState.idle) setState(() => currentState = AppState.listening);
      }
    });

    FlutterBackgroundService().on('matchFound').listen((event) {
      if (mounted) {
        setState(() {
          currentState = AppState.alerting;
          matchedWord = event?['keyword'] ?? 'your name';
        });
        _fetchRandomMeme(); // Get a fresh meme for the teacher screen!
      }
    });
  }

  Future<void> _fetchRandomMeme() async {
    if (isFetchingMeme) return;
    setState(() => isFetchingMeme = true);
    try {
      final response = await http.get(Uri.parse('https://meme-api.com/gimme/CollegeMemes'));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          currentMemeUrl = data['url'];
          currentMemeText = data['title'];
        });
      }
    } catch (e) {
      print('Meme fetch error: $e');
    } finally {
      if (mounted) setState(() => isFetchingMeme = false);
    }
  }

  Future<void> _loadProfile() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      rollNumber = prefs.getString('rollNumber') ?? 'you';
      if (rollNumber.isEmpty || rollNumber == 'you') rollNumber = prefs.getString('userName') ?? 'you';
    });
  }

  void _toggleListening() async {
    if (rollNumber == '...' || rollNumber == 'you') {
      Navigator.push(context, MaterialPageRoute(builder: (_) => const ProfileSetupScreen())).then((_) => _loadProfile());
      return;
    }

    if (currentState == AppState.listening) {
      FlutterBackgroundService().invoke("stopService");
      setState(() => currentState = AppState.idle);
      _fetchRandomMeme(); // refresh meme when going back to idle
    } else {
      final micGranted = await Permission.microphone.request().isGranted;
      final notifGranted = await Permission.notification.request().isGranted;

      if (micGranted && notifGranted) {
        await FlutterBackgroundService().startService();
        setState(() => currentState = AppState.listening);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('drop those mic perms first fr')));
      }
    }
  }

  @override
  void dispose() {
    _waveController.dispose();
    _floatingController.dispose();
    super.dispose();
  }

  Widget _buildTopBar() {
    if (currentState == AppState.alerting) return const SizedBox.shrink();
    
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          if (currentState == AppState.listening)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('rollcall', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 24, color: Colors.white)),
                Text('listening for $rollNumber', style: const TextStyle(color: Colors.pinkAccent, fontWeight: FontWeight.bold)),
              ],
            )
          else
            const SizedBox.shrink(),
          
          Row(
            children: [
               Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(color: Colors.white.withOpacity(0.1), borderRadius: BorderRadius.circular(20)),
                  child: const Text('🔥 7', style: TextStyle(color: Colors.orangeAccent, fontWeight: FontWeight.bold, fontSize: 16)),
                ),
              const SizedBox(width: 16),
              IconButton(
                icon: const Icon(Icons.settings, color: Colors.grey),
                onPressed: () {
                   Navigator.push(context, MaterialPageRoute(builder: (_) => const ProfileSetupScreen())).then((_) => _loadProfile());
                },
              )
            ],
          )
        ],
      ),
    );
  }

  Widget _buildMemeCard() {
    if (currentMemeUrl == null) return const SizedBox.shrink();
    
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 32),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.05),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.3), blurRadius: 20)],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: Column(
          children: [
            if (currentMemeText != null)
              Padding(
                padding: const EdgeInsets.all(12.0),
                child: Text(currentMemeText!, style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
              ),
            Image.network(
              currentMemeUrl!, 
              height: 200, 
              width: double.infinity,
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) => const SizedBox.shrink(),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (currentState == AppState.alerting) {
      return Scaffold(
        backgroundColor: Colors.redAccent.shade700,
        body: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Bouncing Alert Header
              AnimatedBuilder(
                animation: _waveController,
                builder: (context, child) {
                  return Transform.scale(
                    scale: 1.0 + (_waveController.value * 0.1),
                    child: const Text('🚨 SAY PRESENT !!! 🚨', style: TextStyle(fontSize: 36, fontWeight: FontWeight.w900, color: Colors.yellowAccent)),
                  );
                }
              ),
              const SizedBox(height: 8),
              const Text('they just called', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.black87)),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 20),
                decoration: BoxDecoration(
                   color: Colors.black.withOpacity(0.3), 
                   borderRadius: BorderRadius.circular(24),
                   border: Border.all(color: Colors.black.withOpacity(0.5), width: 3)
                ),
                child: Text('"$matchedWord"', style: const TextStyle(fontSize: 48, fontWeight: FontWeight.w900, color: Colors.black)),
              ),
              const SizedBox(height: 32),
              
              // Random Teacher / College Meme
              if (currentMemeUrl != null)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 32),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(24),
                    child: Image.network(
                      currentMemeUrl!,
                      height: 250,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => const Text('👁️👄👁️', style: TextStyle(fontSize: 100)),
                    ),
                  ),
                )
              else
                const Text('👁️👄👁️', style: TextStyle(fontSize: 100)),
                
              const Spacer(),
              Padding(
                padding: const EdgeInsets.all(32.0),
                child: InkWell(
                  onTap: () {
                    setState(() => currentState = AppState.listening);
                  },
                  child: Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    decoration: BoxDecoration(
                      color: Colors.black, 
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: const [BoxShadow(color: Colors.black45, offset: Offset(0, 10), blurRadius: 20)]
                    ),
                    child: const Center(child: Text('DISMISS', style: TextStyle(color: Colors.redAccent, fontSize: 32, fontWeight: FontWeight.w900))),
                  ),
                ),
              )
            ],
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFF0F0F0F),
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            const Spacer(),
            if (currentState == AppState.idle) ...[
              const Text('yo, it\'s', style: TextStyle(fontSize: 40, fontWeight: FontWeight.w900, color: Colors.white)),
              ShaderMask(
                shaderCallback: (bounds) => const LinearGradient(colors: [Color(0xFFFFB75E), Color(0xFFED8F03)]).createShader(bounds),
                child: const Text('rollcall', style: TextStyle(fontSize: 64, fontWeight: FontWeight.w900, color: Colors.white)),
              ),
              const SizedBox(height: 32),
              
              // Floating Animated sleeping cat Lottie
              AnimatedBuilder(
                animation: _floatingController,
                builder: (context, child) {
                  return Transform.translate(
                    offset: Offset(0, math.sin(_floatingController.value * 2 * math.pi) * 10),
                    child: child,
                  );
                },
                child: SizedBox(
                  height: 150,
                  child: Lottie.network(
                    sleepingCatLotties[0], 
                    errorBuilder: (context, error, stackTrace) => const Text('💤🐈📱', style: TextStyle(fontSize: 80)),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text('stop taking L\'s while doom scrolling', textAlign: TextAlign.center, style: TextStyle(color: Colors.grey, fontSize: 16, fontWeight: FontWeight.bold)),
              
              const SizedBox(height: 32),
              // Render random meme
              _buildMemeCard(),
              
            ] else ...[
              Stack(
                alignment: Alignment.center,
                clipBehavior: Clip.none,
                children: [
                   // Vibing Cat Lottie
                   AnimatedBuilder(
                     animation: _floatingController,
                     builder: (context, child) {
                       return Transform.translate(
                         offset: Offset(0, math.sin(_floatingController.value * 2 * math.pi) * 15),
                         child: child,
                       );
                     },
                     child: SizedBox(
                       height: 180,
                       child: Lottie.network(
                         vibingCatLotties[0],
                         errorBuilder: (context, error, stackTrace) => const Text('🎧😺', style: TextStyle(fontSize: 120)),
                       ),
                     ),
                   ),
                   Positioned(
                     top: -20, right: -40,
                     child: Container(
                       padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                       decoration: BoxDecoration(color: Colors.white.withOpacity(0.15), borderRadius: BorderRadius.circular(20)),
                       child: const Text('im listening...', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                     )
                   )
                ],
              ),
              const SizedBox(height: 48),
              Container(
                 padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 14),
                 decoration: BoxDecoration(color: Colors.pink.withOpacity(0.15), borderRadius: BorderRadius.circular(30), border: Border.all(color: Colors.pink.withOpacity(0.4), width: 2)),
                 child: const Row(
                   mainAxisSize: MainAxisSize.min,
                   children: [
                      Text('👀 ••• ears perked', style: TextStyle(color: Colors.pinkAccent, fontWeight: FontWeight.bold, fontSize: 20, letterSpacing: 1.2)),
                   ],
                 )
              ),
              const SizedBox(height: 60),
              AnimatedBuilder(
                animation: _waveController,
                builder: (context, child) {
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(24, (index) {
                      final height = 20.0 + math.sin((_waveController.value * 2 * math.pi) + index) * 35.0;
                      return Container(
                        margin: const EdgeInsets.symmetric(horizontal: 3),
                        width: 8,
                        height: height.abs() + 10,
                        decoration: BoxDecoration(
                           color: HSVColor.fromAHSV(1.0, (index * 15).toDouble(), 0.8, 1.0).toColor(),
                           borderRadius: BorderRadius.circular(10),
                           boxShadow: [
                             BoxShadow(color: HSVColor.fromAHSV(1.0, (index * 15).toDouble(), 0.8, 1.0).toColor().withOpacity(0.5), blurRadius: 10)
                           ]
                        ),
                      );
                    }),
                  );
                }
              ),
              const SizedBox(height: 24),
              const Text('~ reading frequency ~', style: TextStyle(color: Colors.white24, fontWeight: FontWeight.bold, fontSize: 16, letterSpacing: 2)),
            ],
            const Spacer(),
            Padding(
              padding: const EdgeInsets.all(32.0),
              child: GestureDetector(
                onTap: _toggleListening,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  width: double.infinity,
                  height: 120,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(40),
                    gradient: LinearGradient(
                      colors: currentState == AppState.listening 
                         ? [const Color(0xFFFF0844), const Color(0xFFFFB199)]
                         : [const Color(0xFF4CB8C4), const Color(0xFF3CD3AD)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: (currentState == AppState.listening ? Colors.redAccent : Colors.tealAccent).withOpacity(0.4),
                        blurRadius: 40, offset: const Offset(0, 10), spreadRadius: 10
                      )
                    ]
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(currentState == AppState.listening ? Icons.stop_circle_outlined : Icons.waving_hand_rounded, color: Colors.white, size: 40),
                      const SizedBox(height: 8),
                      Text(currentState == AppState.listening ? 'STOP ROLLCALL' : 'START HUSTLE', 
                        style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w900, letterSpacing: 1.5)
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
