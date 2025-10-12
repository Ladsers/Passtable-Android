import 'dart:io';

import 'package:get_it/get_it.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:passtable/features/file_picker/abstract_file_picker.dart';
import 'package:passtable/features/file_picker/android_file_picker.dart';
import 'package:passtable/features/file_picker/default_file_picker.dart';
import 'package:passtable/features/file_repository/abstract_file_repository.dart';
import 'package:passtable/features/file_repository/android_file_repository.dart';
import 'package:passtable/features/file_repository/default_file_repository.dart';
import 'package:passtable/features/recent_files/recent_files.dart';

void initDi({required Box<RecentFile> recentFilesBox}) {
  final isAndroid = Platform.isAndroid;

  GetIt.I.registerLazySingleton(
    () => RecentFilesRepository(recentFilesBox: recentFilesBox),
  );

  GetIt.I.registerLazySingleton<AbstractFilePicker>(
    () => isAndroid ? AndroidFilePicker() : DefaultFilePicker(),
  );

  GetIt.I.registerLazySingleton<AbstractFileRepository>(
    () => isAndroid ? AndroidFileRepository() : DefaultFileRepository(),
  );
}
