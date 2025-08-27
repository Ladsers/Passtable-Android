import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:passtable/features/recent_files/recent_files.dart';

part 'recent_files.state.dart';
part 'recent_files.event.dart';

class RecentFilesBloc extends Bloc<RecentFilesEvent, RecentFilesState> {
  final RecentFilesRepository repository;

  RecentFilesBloc(this.repository) : super(RecentFilesLoading()) {
    on<LoadRecentFiles>((event, emit) async {
      try {
        if (state is! RecentFilesLoaded) emit(RecentFilesLoading());

        final recentFiles = await repository.getRecentFiles();
        if (recentFiles.isNotEmpty) {
          emit(RecentFilesLoaded(recentFiles: recentFiles));
        } else {
          emit(RecentFilesEmpty());
        }
      } catch (e) {
        emit(RecentFilesError());
      }
    });
  }
}
