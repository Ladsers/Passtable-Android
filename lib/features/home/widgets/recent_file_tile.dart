import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:passtable/features/recent_files/models/recent_file.dart';
import 'package:passtable/shared/enums/list_position.dart';

/// A widget that displays a single recent file in a list
/// Shows file name, last opened time, and handles different border styles based on position
class RecentFileTile extends StatelessWidget {
  const RecentFileTile({
    super.key,
    required this.listPosition,
    required this.recentFile,
  });

  /// Position of this item in the list (first, middle, last, or single)
  final ListPosition listPosition;

  /// Data model containing information about the recent file
  final RecentFile recentFile;

  // Border radius constants for visual styling
  static const double _borderRadiusOuter = 16;
  static const double _borderRadiusInner = 4;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    // Determine border radius based on position in list
    final top = listPosition.isFirstOrSingle
        ? _borderRadiusOuter // Outer radius for first or single item
        : _borderRadiusInner; // Inner radius for middle items
    final bottom = listPosition.isLastOrSingle
        ? _borderRadiusOuter // Outer radius for last or single item
        : _borderRadiusInner; // Inner radius for middle items

    // Create date formatter for displaying last opened time
    final dtFormat = DateFormat.yMd().add_jm();

    return Card(
      margin: EdgeInsets.symmetric(horizontal: 0, vertical: 2),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(top),
          bottom: Radius.circular(bottom),
        ),
      ),
      color: theme.colorScheme.surfaceContainerLow,
      elevation: 0, // No shadow
      clipBehavior: Clip.antiAlias, // Clip content to border radius
      child: ListTile(
        contentPadding: EdgeInsets.symmetric(horizontal: 16),
        title: Text(recentFile.name, style: theme.textTheme.bodyMedium),
        subtitle: Text(
          dtFormat.format(
            recentFile.lastOpened.toLocal(),
          ), // Formatted last opened time with local time zone
          style: theme.textTheme.labelSmall,
        ),
        leading: Icon(Icons.insert_drive_file_outlined, size: 30),
        onTap: () =>
            Navigator.pushNamed(context, '/'), // TODO: Navigate to file details
      ),
    );
  }
}
