import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:passtable/features/recent_files/models/recent_file.dart';
import 'package:passtable/shared/enums/list_position.dart';

class RecentFileTile extends StatelessWidget {
  const RecentFileTile({
    super.key,
    required this.listPosition,
    required this.recentFile,
  });

  final ListPosition listPosition;
  final RecentFile recentFile;

  static const double _borderRadiusOuter = 16;
  static const double _borderRadiusInner = 4;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final top = listPosition.isFirstOrSingle
        ? _borderRadiusOuter
        : _borderRadiusInner;
    final bottom = listPosition.isLastOrSingle
        ? _borderRadiusOuter
        : _borderRadiusInner;

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
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      child: ListTile(
        contentPadding: EdgeInsets.symmetric(horizontal: 16),
        title: Text(recentFile.name, style: theme.textTheme.bodyMedium),
        subtitle: Text(
          dtFormat.format(recentFile.lastOpened.toLocal()),
          style: theme.textTheme.labelSmall,
        ),
        leading: Icon(Icons.insert_drive_file_outlined, size: 30),
        onTap: () => Navigator.pushNamed(context, '/'), // todo
      ),
    );
  }
}
