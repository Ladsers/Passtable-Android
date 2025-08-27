import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import 'package:passtable/features/home/bloc/recent_files.bloc.dart';
import 'package:passtable/features/home/widgets/widgets.dart';
import 'package:passtable/features/recent_files/recent_files.dart';
import 'package:passtable/shared/enums/list_position.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _recentFilesBloc = RecentFilesBloc(GetIt.I<RecentFilesRepository>());

  @override
  void initState() {
    _recentFilesBloc.add(LoadRecentFiles());
    super.initState();
  }

  @override
  void dispose() {
    _recentFilesBloc.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surfaceContainerHigh,
      appBar: CustomAppBar(),
      body: Column(
        children: [
          Expanded(child: _buildRecentFilesContainer(_recentFilesBloc)), // todo
          ActionButtonsLayout(isMobileApp: true, isSupportAvailable: true),
        ],
      ),
      bottomNavigationBar: Container(
        height: MediaQuery.of(context).padding.bottom,
        color: Colors.transparent,
      ),
      //backgroundColor: Theme.of(context).colorScheme.surface,
    );
  }

  Widget _buildRecentFilesContainer(RecentFilesBloc recentFilesBloc) {
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
            child: BlocBuilder<RecentFilesBloc, RecentFilesState>(
              bloc: recentFilesBloc,
              builder: (context, state) {
                if (state is RecentFilesLoaded) {
                  return ListView.builder(
                    padding: EdgeInsets.only(
                      left: 24,
                      right: 24,
                      bottom: 24,
                      top: 0,
                    ),
                    itemCount: state.recentFiles.length,
                    itemBuilder: (context, index) {
                      final recentFile = state.recentFiles[index];
                      final listPosition = state.recentFiles.length == 1
                          ? ListPosition.single
                          : index == 0
                          ? ListPosition.first
                          : index == state.recentFiles.length - 1
                          ? ListPosition.last
                          : ListPosition.middle;
                      return RecentFileTile(
                        listPosition: listPosition,
                        recentFile: recentFile,
                      );
                    },
                  );
                }
                if (state is RecentFilesEmpty) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [Text('No files')],
                    ),
                  );
                }
                // todo Error
                return const Column();
              },
            ),
          ),
        ],
      ),
    );
  }
}
