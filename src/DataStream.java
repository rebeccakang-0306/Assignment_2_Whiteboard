public class DataStream {
    int maxStorage = 10000;
    int curIndex = 0;

    void nextIndex() {
        curIndex += 1;
    }
    void initialiseIndex() { curIndex = 0; }
}
