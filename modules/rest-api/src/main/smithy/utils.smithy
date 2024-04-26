$version: "2"

namespace utils

@trait
structure authToken{
  @required
  roles: StringList
}

list StringList{
    member: String
}
