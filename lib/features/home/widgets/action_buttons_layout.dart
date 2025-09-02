import 'dart:io';
import 'package:flutter/material.dart';
import 'package:passtable/features/home/utils/create_tool_list.dart';
import 'package:passtable/features/home/widgets/tool_button.dart';
import 'package:passtable/generated/l10n.dart';

/// A widget that lays out action buttons in a responsive grid
/// Arranges tool buttons based on available screen width and platform
class ActionButtonsLayout extends StatelessWidget {
  const ActionButtonsLayout({super.key});

  // Button width constants
  static const double _buttonWidth = 152; // Standard button width
  static const double _buttonHalfWidth = _buttonWidth / 2;
  // Spacing constants
  static const double _buttonPadding = 12; // Padding between buttons
  static const double _buttonHalfPadding = _buttonPadding / 2;

  // Calculated widths
  static const double _buttonFullWidth =
      _buttonWidth + _buttonPadding; // Full width including padding
  static const double _compactButtonWidth =
      _buttonHalfWidth -
      _buttonHalfPadding; // For a row with compact buttons, because additional space is needed between them

  // Container padding
  static const double _containerPadding = 24; // Padding around the container

  // Layout constants
  static const int _maxNumWidgetsInRow = 4; // Maximum buttons per row
  static const int _minNumWidgetsInRow = 2; // Minimum buttons per row

  @override
  Widget build(BuildContext context) {
    final isMobileApp = Platform.isAndroid || Platform.isIOS;
    final locale = Localizations.localeOf(context);

    // Check if support is available (currently only for Russia)
    // Potential problem: detection by language, not by country code, because locale.countryCode == null.
    final isSupportAvailable = locale.languageCode == 'ru';

    // Create list of tools
    final toolList = createToolList(
      s: S.of(context),
      isMobileApp: isMobileApp,
      isSupportAvailable: isSupportAvailable,
    );

    // Build tool buttons with different layout for Russia
    final toolButtons = !isSupportAvailable
        // Standard layout for other countries
        ? toolList.map((e) => ToolButton(tool: e, width: _buttonWidth)).toList()
        // Special layout for Russia with compact buttons
        : [
            // Regular buttons for all but last two tools
            ...toolList
                .take(toolList.length - 2)
                .map((e) => ToolButton(tool: e, width: _buttonWidth)),
            // Row with two compact buttons for the last two tools
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
        // Calculate how many buttons fit in a row based on available width
        final int buttonsPerRow = (constraints.maxWidth / _buttonFullWidth)
            .floor();

        // Determine if we need one or two rows
        final int rowCount = buttonsPerRow >= _maxNumWidgetsInRow ? 1 : 2;

        // Calculate how many buttons go in the first row
        final int firstRowButtons = buttonsPerRow >= _maxNumWidgetsInRow
            ? _maxNumWidgetsInRow // Fill first row completely if enough space
            : buttonsPerRow >= (_maxNumWidgetsInRow - 1)
            ? _maxNumWidgetsInRow -
                  1 // One less if almost enough space
            : _minNumWidgetsInRow; // Minimum buttons otherwise

        return Padding(
          padding: EdgeInsets.only(
            top: _containerPadding,
            bottom: _containerPadding,
            left: (rowCount > 1
                ? 0
                : _containerPadding), // Left padding only for single row
            right: 0, // No right padding
          ),
          child: Column(
            // Align content based on available width
            crossAxisAlignment: buttonsPerRow >= _maxNumWidgetsInRow
                ? CrossAxisAlignment
                      .start // Left-align for wide screens
                : CrossAxisAlignment.center, // Center for narrow screens
            children: [
              // First row of buttons
              Row(
                spacing: rowCount > 1
                    ? 0
                    : _buttonPadding, // No spacing for multi-row
                mainAxisAlignment: buttonsPerRow >= _maxNumWidgetsInRow
                    ? MainAxisAlignment
                          .start // Left-align for wide screens
                    : MainAxisAlignment
                          .spaceEvenly, // Space evenly for narrow screens
                children: [
                  for (int i = 0; i < firstRowButtons; i++) toolButtons[i],
                ],
              ),
              if (rowCount > 1)
                const SizedBox(
                  height: _buttonPadding,
                ), // Add space between rows if needed
              if (rowCount > 1)
                // Second row of buttons (only when needed)
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    for (int i = firstRowButtons; i < _maxNumWidgetsInRow; i++)
                      toolButtons[i], // Add remaining buttons to second row
                  ],
                ),
            ],
          ),
        );
      },
    );
  }
}
