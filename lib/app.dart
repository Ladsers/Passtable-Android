import 'package:flutter/material.dart';
import 'package:passtable/shared/router.dart';
import 'package:passtable/shared/theme/theme.dart';

class PasstableApp extends StatelessWidget {
  const PasstableApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(title: 'Passtable', theme: lightTheme, routes: routes);
  }
}
