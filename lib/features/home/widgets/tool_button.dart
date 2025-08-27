import 'package:flutter/material.dart';
import 'package:passtable/features/home/enums/tool_priority.dart';
import 'package:passtable/features/home/models/tool.dart';

class ToolButton extends StatelessWidget {
  const ToolButton({super.key, required this.tool, required this.width});

  final Tool tool;
  final double width;

  static const double _buttonHeight = 48;
  static const double _borderRadius = _buttonHeight / 2;

  ButtonStyle get _baseStyle => ButtonStyle(
    minimumSize: WidgetStateProperty.all(Size(width, _buttonHeight)),
    shape: WidgetStateProperty.all(
      RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(_borderRadius),
      ),
    ),
    //elevation: WidgetStateProperty.all(0),
    //shadowColor: WidgetStateProperty.all(Colors.transparent),
  );

  @override
  Widget build(BuildContext context) {
    final child = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(tool.icon, size: 20),
        if (tool.name != null) const SizedBox(width: 8),
        if (tool.name != null)
          Text(
            tool.name!,
            textAlign: TextAlign.center,
            style: TextStyle(height: 1.1),
          ),
      ],
    );

    final button = switch (tool.priority) {
      ToolPriority.high => FilledButton(
        onPressed: () => Navigator.pushNamed(context, tool.route),
        style: _baseStyle,
        child: child,
      ),
      ToolPriority.medium => FilledButton.tonal(
        onPressed: () => Navigator.pushNamed(context, tool.route),
        style: _baseStyle.copyWith(
          backgroundColor: WidgetStateProperty.all(
            Theme.of(context).colorScheme.inversePrimary,
          ),
          foregroundColor: WidgetStateProperty.all(
            Theme.of(context).colorScheme.inverseSurface,
          ),
        ),
        child: child,
      ),
      ToolPriority.low => OutlinedButton(
        onPressed: () => Navigator.pushNamed(context, tool.route),
        style: _baseStyle.copyWith(
          side: WidgetStateProperty.all(
            BorderSide(
              color: Theme.of(context).colorScheme.primary.withAlpha(64),
              width: 1.5,
            ),
          ),
        ),
        child: child,
      ),
    };

    return tool.tooltip != null
        ? Tooltip(message: tool.tooltip!, child: button)
        : button;
  }
}
