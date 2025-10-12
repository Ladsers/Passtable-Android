abstract class AbstractFileRepository {
  Future writeData({required String filePath, required String data});
  Future<String> readData({required String filePath});
  Future<String> createFile({
    required String directoryPath,
    required String fileName,
  });
}
