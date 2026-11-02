import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'listening_service.dart';
import 'ui/main_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeService();
  runApp(const SeventyFivePercApp());
}

class SeventyFivePercApp extends StatelessWidget {
  const SeventyFivePercApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '75%',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0F0F1A),
        textTheme: GoogleFonts.outfitTextTheme(ThemeData.dark().textTheme),
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF7E57C2), // Deep purple
          brightness: Brightness.dark,
          primary: const Color(0xFFA27BFF),
          secondary: const Color(0xFF00FFC2), // Neon Cyan accent
          surface: const Color(0xFF1E1E2E), // Glass surface base
        ),
        useMaterial3: true,
      ),
      home: const MainScreen(),
    );
  }
}
