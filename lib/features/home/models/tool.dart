import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';

class Tool {
  final String? name;
  final String? tooltip;
  final IconData icon;
  final ToolPriority priority;
  final String route;

  const Tool({
    this.name,
    this.tooltip,
    required this.icon,
    required this.priority,
    required this.route,
  });
}
