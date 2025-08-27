import 'package:flutter/material.dart';

class CustomAppBar extends StatelessWidget implements PreferredSizeWidget {
  const CustomAppBar({super.key});

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
        icon: const Icon(Icons.system_update_alt_rounded, color: Colors.deepOrange),
        onPressed: () {},
      ),
      actions: [IconButton(icon: const Icon(Icons.settings), onPressed: () {})],
    );
  }
}
