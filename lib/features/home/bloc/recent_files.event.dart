part of 'recent_files.bloc.dart';

abstract class RecentFilesEvent extends Equatable {}

class LoadRecentFiles extends RecentFilesEvent {
  @override
  List<Object?> get props => [];
}

//class RefreshRecentFiles extends RecentFilesEvent {
//  @override
//  List<Object?> get props => [];
//}
