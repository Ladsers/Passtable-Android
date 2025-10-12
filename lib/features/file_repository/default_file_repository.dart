import 'package:passtable/features/file_repository/abstract_file_repository.dart';

class DefaultFileRepository implements AbstractFileRepository {
  @override
  Future writeData({required String filePath, required String data}) {
    // TODO: implement writeData
    throw UnimplementedError();
  }

  @override
  Future<String> readData({required String filePath}) {
    // TODO: implement readData
    throw UnimplementedError();
  }

  @override
  Future<String> createFile({required String directoryPath, required String fileName}) {
    // TODO: implement createFile
    throw UnimplementedError();
  }
}