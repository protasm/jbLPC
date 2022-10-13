#include <stdio.h>

/* test */ #define BAR "bar"

int main(int argc, const char* argv[]) {
  int i = 0;
  #define FOO "foo"
  printf("foo\n");
  printf(BAR);
}

