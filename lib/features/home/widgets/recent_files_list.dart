import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:passtable/features/home/bloc/recent_files.bloc.dart';
import 'package:passtable/features/home/widgets/recent_file_tile.dart';
import 'package:passtable/features/recent_files/recent_files.dart';
import 'package:passtable/generated/l10n.dart';
import 'package:passtable/shared/enums/list_position.dart';

class RecentFilesList extends StatelessWidget {
  const RecentFilesList({super.key});

  @override
  Widget build(BuildContext context) {
    final bloc = context.read<RecentFilesBloc>();
    return BlocBuilder<RecentFilesBloc, RecentFilesState>(
      bloc: bloc,
      builder: (context, state) {
        if (state is RecentFilesLoaded) {
          return _buildListView(state.recentFiles);
        } else if (state is RecentFilesEmpty) {
          return _buildEmptyState(context);
        } else {
          return _buildErrorState();
        }
      },
    );
  }

  Widget _buildListView(List<RecentFile> recentFiles) {
    return ListView.builder(
      itemCount: recentFiles.length,
      itemBuilder: (context, index) {
        final listPosition = _calculateListPosition(index, recentFiles.length);
        return RecentFileTile(
          listPosition: listPosition,
          recentFile: recentFiles[index],
        );
      },
    );
  }

  ListPosition _calculateListPosition(int index, int totalItems) {
    if (totalItems == 1) return ListPosition.single;
    if (index == 0) return ListPosition.first;
    if (index == totalItems - 1) return ListPosition.last;
    return ListPosition.middle;
  }

  Widget _buildEmptyState(BuildContext context) {
    return Center(child: Text(S.of(context).home_text_noRecentlyOpenedFiles));
  }

  Widget _buildErrorState() {
    return const Center(child: Text('Error')); // TODO
  }
}
