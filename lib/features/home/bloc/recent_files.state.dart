part of 'recent_files.bloc.dart';

abstract class RecentFilesState extends Equatable {}

class RecentFilesLoading extends RecentFilesState {
  @override
  List<Object?> get props => [];
}

class RecentFilesLoaded extends RecentFilesState {
  final List<RecentFile> recentFiles;

  RecentFilesLoaded({required this.recentFiles});

  @override
  List<Object?> get props => [recentFiles];
}

class RecentFilesError extends RecentFilesState {
  @override
  List<Object?> get props => [];
}

class RecentFilesEmpty extends RecentFilesState {
  @override
  List<Object?> get props => [];
}

//class RecentFilesRefreshing extends RecentFilesState {
//  @override
//  List<Object?> get props => [];
//}
