import 'package:flutter/material.dart';
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
      appBar: CustomAppBar(),
      body: _buildBody(context),
      bottomNavigationBar: Container(
        height: MediaQuery.of(context).padding.bottom,
        color: Colors.transparent,
      ),
      //backgroundColor: Theme.of(context).colorScheme.surface,
    );
  }

  Widget _buildBody(BuildContext context) {
    return Column(
      children: [
        Expanded(child: _buildRecentFilesContainer()),
        ActionButtonsLayout(isMobileApp: true, isSupportAvailable: true),
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
