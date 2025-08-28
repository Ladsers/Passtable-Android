import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:passtable/generated/l10n.dart';
import 'package:passtable/shared/router.dart';
import 'package:passtable/shared/theme/theme.dart';

class PasstableApp extends StatelessWidget {
  const PasstableApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Passtable',
      theme: lightTheme,
      routes: routes,
      localizationsDelegates: [
        S.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: S.delegate.supportedLocales,
    );
  }
}
