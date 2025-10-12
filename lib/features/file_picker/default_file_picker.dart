import 'package:file_picker/file_picker.dart';
import 'package:passtable/features/file_picker/abstract_file_picker.dart';

class DefaultFilePicker implements AbstractFilePicker {
  @override
  Future<String?> pickFile() async {
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        allowMultiple: false,
        type: FileType.custom,
        allowedExtensions: ['passtable'],
        lockParentWindow: true,
      );

      return result?.files.single.path;
    } on Exception catch (e) {
      // TODO
      //throw;
      return null;
    }
  }

  @override
  Future<String?> pickDirectory() async {
    try {
      return await FilePicker.platform.getDirectoryPath(lockParentWindow: true);
    } on Exception catch (e) {
      // TODO
      //throw;
      return null;
    }
  }
}
