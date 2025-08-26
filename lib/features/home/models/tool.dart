import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';

class Tool {
  final String label;
  final IconData icon;
  final ToolPriority priority;
  final String route;

  const Tool({
    required this.label,
    required this.icon,
    required this.priority,
    required this.route,
  });
}
