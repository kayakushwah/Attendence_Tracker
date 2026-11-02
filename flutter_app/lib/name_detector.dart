class DetectionResult {
  final bool matched;
  final String matchedKeyword;
  final double confidence;
  final String matchedText;

  DetectionResult({
    required this.matched,
    required this.matchedKeyword,
    required this.confidence,
    required this.matchedText,
  });
}

class NameDetector {
  List<String> keywords = [];
  double sensitivity = 0.7;

  // Number words mapping for roll call detection
  final Map<String, String> numberWords = {
    "zero": "0", "one": "1", "two": "2", "three": "3",
    "four": "4", "five": "5", "six": "6", "seven": "7",
    "eight": "8", "nine": "9", "ten": "10", "eleven": "11",
    "twelve": "12", "thirteen": "13", "fourteen": "14",
    "fifteen": "15", "sixteen": "16", "seventeen": "17",
    "eighteen": "18", "nineteen": "19", "twenty": "20",
    "thirty": "30", "forty": "40", "fifty": "50",
    "sixty": "60", "seventy": "70", "eighty": "80", "ninety": "90",
    "hundred": "100"
  };

  void setKeywords(String name, String rollNumber, String extraKeywords) {
    Set<String> allKeywords = {};

    if (name.isNotEmpty) {
      allKeywords.add(name.trim().toLowerCase());
      final parts = name.trim().split(' ');
      if (parts.isNotEmpty && parts.first.length >= 3) {
        allKeywords.add(parts.first.toLowerCase());
      }
    }

    if (rollNumber.isNotEmpty) {
      allKeywords.add(rollNumber.trim().toLowerCase());
      // Add without leading zeros: "007" → "7"
      final stripped = rollNumber.trimLeft().replaceFirst(RegExp(r'^0+'), '');
      if (stripped.isNotEmpty) {
        allKeywords.add(stripped.toLowerCase());
      }
    }

    if (extraKeywords.isNotEmpty) {
      for (var kw in extraKeywords.split(',')) {
        final trimmed = kw.trim().toLowerCase();
        if (trimmed.isNotEmpty) allKeywords.add(trimmed);
      }
    }

    keywords = allKeywords.toList();
  }

  void setSensitivity(String level) {
    if (level == "low") {
      sensitivity = 0.5;
    } else if (level == "high") {
      sensitivity = 0.9;
    } else {
      sensitivity = 0.7;
    }
  }

  DetectionResult analyze(String transcript) {
    if (keywords.isEmpty || transcript.trim().isEmpty) {
      return DetectionResult(matched: false, matchedKeyword: "", confidence: 0, matchedText: "");
    }

    final normalizedTranscript = transcript.toLowerCase().trim();
    final transcriptWithNumbers = _convertNumberWords(normalizedTranscript);

    for (var keyword in keywords) {
      if (normalizedTranscript.contains(keyword) || transcriptWithNumbers.contains(keyword)) {
        return DetectionResult(matched: true, matchedKeyword: keyword, confidence: 1.0, matchedText: transcript);
      }

      final words = normalizedTranscript.split(RegExp(r'\s+'));
      for (var word in words) {
        final distance = _levenshteinDistance(word, keyword);
        final maxLen = word.length > keyword.length ? word.length : keyword.length;
        if (maxLen == 0) continue;

        final similarity = 1.0 - (distance / maxLen);

        if (similarity >= sensitivity) {
          return DetectionResult(matched: true, matchedKeyword: keyword, confidence: similarity, matchedText: word);
        }
      }

      final keywordParts = keyword.split(RegExp(r'\s+'));
      if (keywordParts.length > 1) {
        for (var i = 0; i <= words.length - keywordParts.length; i++) {
          final chunk = words.sublist(i, i + keywordParts.length).join(" ");
          final distance = _levenshteinDistance(chunk, keyword);
          final maxLen = chunk.length > keyword.length ? chunk.length : keyword.length;
          final similarity = 1.0 - (distance / maxLen);

          if (similarity >= sensitivity) {
            return DetectionResult(matched: true, matchedKeyword: keyword, confidence: similarity, matchedText: chunk);
          }
        }
      }
    }

    return DetectionResult(matched: false, matchedKeyword: "", confidence: 0, matchedText: "");
  }

  String _convertNumberWords(String text) {
    var result = text;
    final tensWords = ["twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"];
    final onesWords = ["one", "two", "three", "four", "five", "six", "seven", "eight", "nine"];

    for (var tens in tensWords) {
      for (var ones in onesWords) {
        final tensVal = int.tryParse(numberWords[tens] ?? "");
        final onesVal = int.tryParse(numberWords[ones] ?? "");
        if (tensVal != null && onesVal != null) {
          final combined = (tensVal + onesVal).toString();
          result = result.replaceAll("$tens $ones", combined);
          result = result.replaceAll("$tens-$ones", combined);
        }
      }
    }

    numberWords.forEach((word, digit) {
      result = result.replaceAll(RegExp('\\b$word\\b'), digit);
    });

    return result;
  }

  int _levenshteinDistance(String s1, String s2) {
    final m = s1.length;
    final n = s2.length;
    var dp = List.generate(m + 1, (i) => List.filled(n + 1, 0));

    for (var i = 0; i <= m; i++) dp[i][0] = i;
    for (var j = 0; j <= n; j++) dp[0][j] = j;

    for (var i = 1; i <= m; i++) {
      for (var j = 1; j <= n; j++) {
        final cost = (s1[i - 1] == s2[j - 1]) ? 0 : 1;
        dp[i][j] = [
          dp[i - 1][j] + 1,       // deletion
          dp[i][j - 1] + 1,       // insertion
          dp[i - 1][j - 1] + cost  // substitution
        ].reduce((curr, next) => curr < next ? curr : next);
      }
    }

    return dp[m][n];
  }
}
