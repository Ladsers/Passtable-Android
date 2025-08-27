import 'package:equatable/equatable.dart';
import 'package:hive_flutter/hive_flutter.dart';

part 'recent_file.g.dart';

@HiveType(typeId: 0)
class RecentFile extends Equatable {
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

  @override
  List<Object?> get props => [path, name, lastOpened];
}
