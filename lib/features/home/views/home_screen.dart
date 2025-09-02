import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import 'package:passtable/features/home/bloc/recent_files.bloc.dart';
import 'package:passtable/features/home/widgets/widgets.dart';
import 'package:passtable/features/recent_files/recent_files.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _recentFilesBloc = RecentFilesBloc(GetIt.I<RecentFilesRepository>());

  @override
  void initState() {
    _recentFilesBloc.add(LoadRecentFiles());
    super.initState();
  }

  @override
  void dispose() {
    _recentFilesBloc.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surfaceContainerHigh,
      appBar: CustomAppBar(),
      body: Column(
        children: [
          BlocProvider.value(
            value: _recentFilesBloc,
            child: Expanded(child: RecentFilesContainer()),
          ),
          ActionButtonsLayout(),
        ],
      ),
      bottomNavigationBar: Container(
        height: MediaQuery.of(context).padding.bottom,
        color: Colors.transparent,
      ),
      //backgroundColor: Theme.of(context).colorScheme.surface,
    );
  }
}
