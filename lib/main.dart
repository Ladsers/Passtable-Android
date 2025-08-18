import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:passtable/app.dart';
import 'package:passtable/features/recent_files/recent_files.dart';

const recentFilesBoxName = 'recent_files';

void main() async {
  await Hive.initFlutter();
  Hive.registerAdapter(RecentFileAdapter());
  final recentFilesBox = await Hive.openBox<RecentFile>(recentFilesBoxName);

  //todo
  GetIt.I.registerLazySingleton(
    () => RecentFilesRepository(recentFilesBox: recentFilesBox),
  );

  runApp(const PasstableApp());
}
