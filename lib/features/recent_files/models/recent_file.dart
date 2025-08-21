import 'package:hive_flutter/hive_flutter.dart';

part 'recent_file.g.dart';

@HiveType(typeId: 0)
class RecentFile {
  @HiveField(0)
  final String path;

  @HiveField(1)
  final String name;

  @HiveField(2)
  final DateTime lastOpened;

  const RecentFile({
    required this.path,
    required this.name,
    required this.lastOpened,
  });
}
