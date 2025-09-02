import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';

/// Represents a tool in the application's home screen
///
/// The Tool class is a data model that defines the properties and behavior
/// of individual tools available in the Passtable application. Each tool
/// represents a feature or functionality that users can access from the home screen.
///
/// Properties:
/// - [name]: The display name of the tool (optional)
/// - [tooltip]: A descriptive text shown when hovering over the tool (optional)
/// - [icon]: The visual representation of the tool using Material Design icons
/// - [priority]: Tools with higher priority will be displayed more prominently.
/// - [route]: The navigation route/path used to access the tool's feature
class Tool {
  /// The display name of the tool shown to users
  /// Can be null if the tool is identified only by its icon
  final String? name;

  /// A descriptive text that appears as a tooltip when users hover over the tool
  final String? tooltip;

  /// The Material Design icon used to visually represent the tool
  /// Required for all tools to ensure consistent visual appearance
  final IconData icon;

  /// Tools with higher priority will be displayed more prominently.
  final ToolPriority priority;

  /// The navigation route/path that determines which screen or feature to open
  /// when the tool is clicked by the user
  final String route;

  /// Creates a new Tool instance with the specified properties
  const Tool({
    this.name,
    this.tooltip,
    required this.icon,
    required this.priority,
    required this.route,
  });
}
