package flash.mounts.common.util;

public class NumberUtil {

  public static int wrapIncr(int number, int min, int max) {
    if (number < max) return number+1;
    else return min;
  }

  public static int wrapIncrOver(int number, int min, int max) {
    if (min < max) {
      if (number < max) return number + 1;
      else return (max - min) % number + 1;
    } else {
      return number + 1;
    }
  }

}
