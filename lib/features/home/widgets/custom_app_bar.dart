import 'package:flutter/material.dart';

/// A custom app bar widget for the application
/// Implements PreferredSizeWidget to define its preferred size
/// Features app title, update button, and settings button
class CustomAppBar extends StatelessWidget implements PreferredSizeWidget {
  const CustomAppBar({super.key});

  /// Define the preferred height of the app bar
  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      title: const Text('Passtable'),
      centerTitle: true,
      elevation: 0,
      scrolledUnderElevation: 0,
      backgroundColor: Theme.of(context).colorScheme.surfaceContainerHigh,
      leading: IconButton(
        icon: const Icon(
          Icons.system_update_alt_rounded,
          color: Colors.deepOrange, // TODO Check color
        ),
        onPressed: () {}, // TODO: Implement update functionality
      ),
      actions: [
        IconButton(
          icon: const Icon(Icons.settings),
          onPressed: () {}, // TODO: Implement settings page
        ),
      ],
    );
  }
}
