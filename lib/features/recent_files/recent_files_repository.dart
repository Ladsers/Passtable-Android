import 'package:hive_flutter/hive_flutter.dart';
import 'package:passtable/features/recent_files/models/models.dart';

/// A repository class that handles storage and management of recent files.
///
/// This class uses Hive Flutter for local storage to persistently store recent files
/// information across app sessions. It maintains a maximum limit of 20 recent files
/// and automatically removes the least recently used file when the limit is exceeded.
///
/// Key functionalities:
/// - Adds new recent files
/// - Persists files locally using Hive
/// - Maintains file order based on last opened time
/// - Enforces a maximum limit on stored files
class RecentFilesRepository {
  static const _maxRecentFiles = 20;

  final Box<RecentFile> _box;

  /// Creates a new instance of RecentFilesRepository with a reference to the Hive box
  /// where recent files will be stored.
  RecentFilesRepository({required Box<RecentFile> recentFilesBox})
    : _box = recentFilesBox;

  /// Adds a new file to the list of recent files.
  ///
  /// If the number of recent files exceeds the maximum limit, the least recently used
  /// file is automatically removed.
  Future<void> addRecentFile({required String path, required String name}) async {
    await _box.put(
      path,
      RecentFile(path: path, name: name, lastOpened: DateTime.now()),
    );

    if (_box.length > _maxRecentFiles) {
      final sorted = _box.values.toList()
        ..sort((a, b) => b.lastOpened.compareTo(a.lastOpened));
      await _box.delete(sorted.last.path);
    }
  }

  /// Retrieves a sorted list of all recently opened files.
  ///
  /// The files are sorted in descending order based on their last opened time.
  Future<List<RecentFile>> getRecentFiles() async {
    return _box.values.toList()
      ..sort((a, b) => b.lastOpened.compareTo(a.lastOpened));
  }
}
