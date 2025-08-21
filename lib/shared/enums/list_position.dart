enum ListPosition {
  first,
  last,
  middle,
  single;

  bool get isFirst => this == ListPosition.first;
  bool get isLast => this == ListPosition.last;
  bool get isMiddle => this == ListPosition.middle;
  bool get isSingle => this == ListPosition.single;

  bool get isFirstOrSingle => isFirst || isSingle;
  bool get isLastOrSingle => isLast || isSingle;
  bool get isEdge => isFirst || isLast || isSingle;
}
