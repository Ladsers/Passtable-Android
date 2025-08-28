import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';
import 'package:passtable/features/home/models/tool.dart';
import 'package:passtable/generated/l10n.dart';

List<Tool> createToolList({
  required S s,
  required bool isMobileApp,
  required bool isSupportAvailable,
}) {
  final list = <Tool>[];

  list.add(
    Tool(
      name: s.home_btn_open,
      icon: Icons.folder_open_rounded,
      priority: ToolPriority.high,
      route: '',
    ),
  );

  list.add(
    Tool(
      name: s.home_btn_create,
      icon: Icons.note_add_rounded,
      priority: ToolPriority.medium,
      route: '',
    ),
  );

  list.add(
    Tool(
      name: s.home_btn_passwordGenerator,
      icon: Icons.auto_awesome_rounded,
      priority: ToolPriority.low,
      route: '',
    ),
  );

  if (isMobileApp) {
    list.add(
      Tool(
        name: isSupportAvailable ? null : s.home_btn_appForPc,
        tooltip: isSupportAvailable ? s.home_btn_appForPc : null,
        icon: Icons.desktop_mac_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  } else {
    list.add(
      Tool(
        name: isSupportAvailable ? null : s.home_btn_androidApp,
        tooltip: isSupportAvailable ? s.home_btn_androidApp : null,
        icon: Icons.phone_android_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  }

  if (isSupportAvailable) {
    list.add(
      Tool(
        tooltip: s.home_btn_supportDeveloper,
        icon: Icons.diamond_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  }

  return list;
}
