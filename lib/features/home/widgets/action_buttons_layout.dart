import 'package:flutter/material.dart';
import 'package:passtable/features/home/utils/create_tool_list.dart';
import 'package:passtable/features/home/widgets/tool_button.dart';
import 'package:passtable/generated/l10n.dart';

class ActionButtonsLayout extends StatelessWidget {
  const ActionButtonsLayout({
    super.key,
    required this.isMobileApp,
    required this.isSupportAvailable,
  });

  static const double _buttonWidth = 152;
  static const double _buttonHalfWidth = _buttonWidth / 2;

  static const double _buttonPadding = 12;
  static const double _buttonHalfPadding = _buttonPadding / 2;

  static const double _buttonFullWidth = _buttonWidth + _buttonPadding;
  static const double _compactButtonWidth =
      _buttonHalfWidth - _buttonHalfPadding;

  static const double _containerPadding = 24;

  static const int _maxNumWidgetsInRow = 4;
  static const int _minNumWidgetsInRow = 2;

  final bool isMobileApp;
  final bool isSupportAvailable;

  @override
  Widget build(BuildContext context) {
    final toolList = createToolList(
      s: S.of(context),
      isMobileApp: isMobileApp,
      isSupportAvailable: isSupportAvailable,
    );

    final toolButtons = !isSupportAvailable
        ? toolList.map((e) => ToolButton(tool: e, width: _buttonWidth)).toList()
        : [
            ...toolList
                .take(toolList.length - 2)
                .map((e) => ToolButton(tool: e, width: _buttonWidth)),
            Row(
              spacing: _buttonPadding,
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ToolButton(
                  tool: toolList[toolList.length - 2],
                  width: _compactButtonWidth,
                ),
                ToolButton(
                  tool: toolList[toolList.length - 1],
                  width: _compactButtonWidth,
                ),
              ],
            ),
          ].toList();

    return LayoutBuilder(
      builder: (context, constraints) {
        final int buttonsPerRow = (constraints.maxWidth / _buttonFullWidth)
            .floor();
        final int rowCount = buttonsPerRow >= _maxNumWidgetsInRow ? 1 : 2;
        final int firstRowButtons = buttonsPerRow >= _maxNumWidgetsInRow
            ? _maxNumWidgetsInRow
            : buttonsPerRow >= (_maxNumWidgetsInRow - 1)
            ? _maxNumWidgetsInRow - 1
            : _minNumWidgetsInRow;

        return Padding(
          padding: EdgeInsets.only(
            top: _containerPadding,
            bottom: _containerPadding,
            left: (rowCount > 1 ? 0 : _containerPadding),
            right: 0,
          ),
          child: Column(
            crossAxisAlignment: buttonsPerRow >= _maxNumWidgetsInRow
                ? CrossAxisAlignment.start
                : CrossAxisAlignment.center,
            children: [
              Row(
                spacing: rowCount > 1 ? 0 : _buttonPadding,
                mainAxisAlignment: buttonsPerRow >= _maxNumWidgetsInRow
                    ? MainAxisAlignment.start
                    : MainAxisAlignment.spaceEvenly,
                children: [
                  for (int i = 0; i < firstRowButtons; i++) toolButtons[i],
                ],
              ),
              if (rowCount > 1) const SizedBox(height: _buttonPadding),
              if (rowCount > 1)
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    for (int i = firstRowButtons; i < _maxNumWidgetsInRow; i++)
                      toolButtons[i],
                  ],
                ),
            ],
          ),
        );
      },
    );
  }
}
