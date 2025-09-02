import 'package:flutter/material.dart';
import 'package:passtable/generated/l10n.dart';

class RecentFilesHeader extends StatelessWidget {
  const RecentFilesHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 24, top: 24, right: 24, bottom: 12),
      child: Text(
        S.of(context).home_label_recentFiles,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w400,
          color: Colors.black,
        ),
      ),
    );
  }
}
