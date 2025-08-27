import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';
import 'package:passtable/features/home/models/tool.dart';

List<Tool> createToolList({
  required bool isMobileApp,
  required bool isSupportAvailable,
}) {
  final list = <Tool>[];

  list.add(
    Tool(
      name: 'Open',
      icon: Icons.folder_open_rounded,
      priority: ToolPriority.high,
      route: '',
    ),
  );

  list.add(
    Tool(
      name: 'Create',
      icon: Icons.note_add_rounded,
      priority: ToolPriority.medium,
      route: '',
    ),
  );

  list.add(
    Tool(
      name: 'Password\ngenerator',
      icon: Icons.auto_awesome_rounded,
      priority: ToolPriority.low,
      route: '',
    ),
  );

  if (isMobileApp) {
    list.add(
      Tool(
        name: isSupportAvailable ? null : 'App for PC',
        tooltip: isSupportAvailable ? 'App for PC' : null,
        icon: Icons.desktop_mac_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  } else {
    list.add(
      Tool(
        name: isSupportAvailable ? null : 'Android\napp',
        tooltip: isSupportAvailable ? 'Android app' : null,
        icon: Icons.phone_android_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  }

  if (isSupportAvailable) {
    list.add(
      Tool(
        tooltip: 'Support developer',
        icon: Icons.diamond_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  }

  return list;
}
