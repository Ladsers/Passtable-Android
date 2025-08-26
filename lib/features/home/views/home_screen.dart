import 'package:flutter/material.dart';
import 'package:passtable/features/home/utils/create_tool_list.dart';
import 'package:passtable/features/home/widgets/widgets.dart';
import 'package:passtable/features/recent_files/models/recent_file.dart';
import 'package:passtable/shared/enums/list_position.dart';

class HomeScreen extends StatelessWidget {
  HomeScreen({super.key});

  // todo test
  final recentFiles = [
    RecentFile(path: '/sda', name: 'File 1', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 3', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 2', lastOpened: DateTime.now()),
    RecentFile(path: '/sda', name: 'File 4', lastOpened: DateTime.now()),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surfaceContainerHigh,
      appBar: _buildAppBar(context),
      body: _buildBody(context),
      bottomNavigationBar: Container(
        height: MediaQuery.of(context).padding.bottom,
        color: Colors.transparent,
      ),
      //backgroundColor: Theme.of(context).colorScheme.surface,
    );
  }

  PreferredSizeWidget _buildAppBar(BuildContext context) {
    return AppBar(
      title: const Text('Passtable'),
      centerTitle: true,
      elevation: 0,
      scrolledUnderElevation: 0,
      backgroundColor: Theme.of(context).colorScheme.surfaceContainerHigh,
      leading: IconButton(icon: const Icon(Icons.update), onPressed: () {}),
      actions: [IconButton(icon: const Icon(Icons.settings), onPressed: () {})],
    );
  }

  Widget _buildBody(BuildContext context) {
    return Column(
      children: [
        Expanded(child: _buildRecentFilesContainer()),
        AdaptiveButtonGrid(),
      ],
    );
  }

  Widget _buildRecentFilesContainer() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
      ),
      width: double.infinity,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(
              left: 24,
              top: 24,
              right: 24,
              bottom: 12,
            ),
            child: Text(
              "Recent files",
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w400,
                color: Colors.black,
              ),
            ),
          ),
          Expanded(
            child: ListView.builder(
              padding: EdgeInsets.only(left: 24, right: 24, bottom: 24, top: 0),
              itemCount: recentFiles.length,
              itemBuilder: (context, index) {
                final recentFile = recentFiles[index];
                final listPosition = recentFiles.length == 1
                    ? ListPosition.single
                    : index == 0
                    ? ListPosition.first
                    : index == recentFiles.length - 1
                    ? ListPosition.last
                    : ListPosition.middle;
                return RecentFileTile(
                  listPosition: listPosition,
                  recentFile: recentFile,
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class AdaptiveButtonGrid extends StatelessWidget {
  const AdaptiveButtonGrid({super.key});

  @override
  Widget build(BuildContext context) {
    final toolList = createToolList(true, true);

    return LayoutBuilder(
      builder: (context, constraints) {
        const double buttonWidth = 162.0;
        final int buttonsPerRow = (constraints.maxWidth / buttonWidth).floor();
        final int rowCount = buttonsPerRow >= 4 ? 1 : 2;
        final int firstRowButtons = buttonsPerRow >= 4
            ? 4
            : buttonsPerRow >= 3
            ? 3
            : 2;

        return Padding(
          padding: EdgeInsets.only(
            top: 24,
            bottom: 24,
            left: (rowCount > 1 ? 0 : 24),
            right: 0,
          ),
          child: Column(
            crossAxisAlignment: buttonsPerRow >= 4
                ? CrossAxisAlignment.start
                : CrossAxisAlignment.center,
            children: [
              Row(
                spacing: rowCount > 1 ? 0 : 12,
                mainAxisAlignment: buttonsPerRow >= 4
                    ? MainAxisAlignment.start
                    : MainAxisAlignment.spaceEvenly,
                children: [
                  for (int i = 0; i < firstRowButtons; i++) ToolButton(tool: toolList[i], width: 150)
                ],
              ),
              if (rowCount > 1) const SizedBox(height: 12),
              if (rowCount > 1)
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    for (int i = firstRowButtons; i < 4; i++) ToolButton(tool: toolList[i], width: 150)
                  ],
                ),
            ],
          ),
        );
      },
    );
  }
}
