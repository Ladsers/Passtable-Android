import 'package:flutter/material.dart';
import 'package:passtable/generated/l10n.dart';

/// A widget that displays the header for the recent files section
class RecentFilesHeader extends StatelessWidget {
  const RecentFilesHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      // Padding around the header
      padding: const EdgeInsets.only(left: 24, top: 24, right: 24, bottom: 12),
      child: Text(
        S.of(context).home_label_recentFiles,
        // TODO Use styles from theme
        style: const TextStyle(
          fontSize: 20, // Font size
          fontWeight: FontWeight.w400,
          color: Colors.black,
        ),
      ),
    );
  }
}
