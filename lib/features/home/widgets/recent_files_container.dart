import 'package:flutter/material.dart';
import 'package:passtable/features/home/widgets/recent_files_header.dart';
import 'package:passtable/features/home/widgets/recent_files_list.dart';

/// A container widget that holds the recent files section
class RecentFilesContainer extends StatelessWidget {
  const RecentFilesContainer({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white, // TODO White background
        borderRadius: BorderRadius.circular(24),
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          RecentFilesHeader(),
          Expanded(
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: 24),
              child: RecentFilesList(),
            ),
          ),
        ],
      ),
    );
  }
}
