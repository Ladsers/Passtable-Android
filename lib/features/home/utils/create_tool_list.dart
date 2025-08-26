import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';
import 'package:passtable/features/home/models/tool.dart';

List<Tool> createToolList(bool isMobileApp, bool isRuLocale) {
  final list = <Tool>[];

  list.add(
    Tool(
      label: 'Open',
      icon: Icons.folder_open_rounded,
      priority: ToolPriority.high,
      route: '',
    ),
  );

  list.add(
    Tool(
      label: 'Create',
      icon: Icons.note_add_rounded,
      priority: ToolPriority.medium,
      route: '',
    ),
  );

  list.add(
    Tool(
      label: 'Password\ngenerator',
      icon: Icons.auto_awesome_rounded,
      priority: ToolPriority.low,
      route: '',
    ),
  );

  if (isMobileApp) {
    list.add(
      Tool(
        label: 'App for PC',
        icon: Icons.desktop_mac_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  } else {
    list.add(
      Tool(
        label: 'App for Android',
        icon: Icons.phone_android_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  }

  if (isRuLocale) {
    list.add(
      Tool(
        label: 'Support developer',
        icon: Icons.diamond_rounded,
        priority: ToolPriority.low,
        route: '',
      ),
    );
  }

  return list;
}
