package aibot.util;

import arc.math.geom.*;
import arc.struct.*;

import static java.lang.Math.*;

//kd tr
public class KDTree{
    Seq<Point2> pts;
    Range root;

    public KDTree(Seq<Point2> input){
        pts = input;
        root = new Range(0, pts.size - 1);
        root.medpartition();
    }

    public Point2 getClosestBruteForce(int x, int y){
        Point2 closest = pts.get(0);
        float dis = abs(closest.x - x) + abs(closest.y - y);
        for(int i = 1; i < pts.size; i++){
            var b = pts.get(i);
            float dis2 = abs(b.x - x) + abs(b.y - y);
            if(dis2 < dis){
                dis = dis2;
                closest = b;
            }
        }
        return closest;
    }

    Range getLeaf(Range r, int x, int y){
        Range leaf = r;
        Range p;
        while(!leaf.isleaf){
            if(leaf.axis == 0){
                p = x > leaf.centerpt.x ? leaf.r : leaf.l;
            }else{
                p = y > leaf.centerpt.y ? leaf.r : leaf.l;
            }
            if(p == null){
                break;
            }
            leaf = p;
        }
        return leaf;
    }

    Range getLeaf(Range r, int x, int y, int minsize){
        if(minsize == 1){
            return getLeaf(r, x, y);
        }
        Range leaf = r;
        Range p;
        while(leaf.end - leaf.start > minsize && !leaf.isleaf){
            if(leaf.axis == 0){
                p = x > leaf.centerpt.x ? leaf.r : leaf.l;
            }else{
                p = y > leaf.centerpt.y ? leaf.r : leaf.l;
            }
            if(p == null){
                break;
            }
            leaf = p;
        }
        return leaf;
    }

    public float getClosestBruteForce(int start, int end, int x, int y, Point2 in){

        in.set(pts.get(start));
        float dis = abs(in.x - x) + abs(in.y - y);
        if(end == start){
            return dis;
        }
        Point2 win = new Point2(in.x, in.y), tmp;
        float dis2;
        for(int i = start + 1; i <= end; i++){
            tmp = pts.get(i);
            dis2 = abs(tmp.x - x) + abs(tmp.y - y);
            if(dis2 < dis){
                dis = dis2;
                win = tmp;
            }
        }
        in.set(win);
        return dis;
    }

    int minsize = 20;

    ObjectSet<Range> set = new ObjectSet();

    public Point2 getClosest(int x, int y){
        if(pts.size < minsize){
            //brute force is faster for smaller values owing to less setup.
            return getClosestBruteForce(x, y);
        }
        set.clear();
        Range leaf = getLeaf(root, x, y, minsize);
        Point2 closest = leaf.centerpt;
        Range prev, otherrange;

        float mindist = getClosestBruteForce(leaf.start, leaf.end, x, y, closest);
        float d;
        Point2 other = new Point2();
        while(leaf != root){
            prev = leaf;
            leaf = leaf.parent;
            otherrange = leaf.other(prev);
            //if the other side of the node is closer, then explore that...
            if(otherrange != null && (otherrange.boundsdist(x, y)) < mindist && !set.contains(leaf)){
                set.add(leaf);
                leaf = getLeaf(otherrange, x, y, minsize);
                d = getClosestBruteForce(leaf.start, leaf.end, x, y, other);
                if(d < mindist){
                    closest.set(other);
                    mindist = d;
                }
            }else{
                //factor in current node's position if other side is not applicable
                d = abs(leaf.centerpt.x - x) + abs(leaf.centerpt.y - y);
                if(d < mindist){
                    closest.set(leaf.centerpt);
                    mindist = d;
                }
            }
        }
        return closest;
    }

    public class Range{
        int start, end, center = -1, depth;
        Range l, r, parent;
        boolean isleaf = false;
        int bxmin = 0, bxmax = 999999, bymin = 0, bymax = 999999;
        int xmin = 0, xmax = 999999, ymin = 0, ymax = 999999, xcen, ycen;
        int axis = 0, axisboundary;
        Point2 centerpt;
        //bounds?

        public Range(int start, int end){
            this.start = start;
            this.end = end;
            depth = 0;
        }

        public Range(int start, int end, Range parent){
            this.start = start;
            this.end = end;
            depth = parent.depth + 1;
            this.parent = parent;
            bxmin = xmin = this.parent.xmin;
            bxmax = xmax = this.parent.xmax;
            bymin = ymin = this.parent.ymin;
            bymax = ymax = this.parent.ymax;
            axis = depth % 2;
        }

        Range other(Range child){
            return child == l ? r : l;
        }

        int boundsdist(int x, int y){
            return max(y > ycen ? y - ymax : ymin - y, 0) + max(x > xcen ? x - xmax : xmin - x, 0);
        }

        void medpartition(){
            axis = (bymax - bymin) > (bxmax - bxmin) ? 1 : 0;
            if(end - start == 0){
                center = start;
                isleaf = true;
                centerpt = pts.get(center);
                xmin = xmax = centerpt.x;
                ymin = ymax = centerpt.y;
                return; //nothing to partition
            }
            if(end - start == 1){
                insertSort(start, end, axis);
                center = end;
                l = new Range(start, start, this);
                l.medpartition();
                if(axis == 0){
                    l.axisboundary = pts.get(center).x;
                }else{
                    l.axisboundary = pts.get(center).y;
                }
                centerpt = pts.get(center);
                recalcBounds();
                return; //partition other to left side
            }
            int pivotIndex = quickselect(start, end, (end + start) / 2, axis);
            pivotIndex = partition(start, end, pivotIndex, pivotIndex, axis);
            center = pivotIndex;
            if(center == end){
                center--;
            }
            centerpt = pts.get(center);
            //make kids and set their boundarys
            l = new Range(start, center - 1, this);
            r = new Range(center + 1, end, this);
            if(axis == 0){
                l.bxmax = l.axisboundary = pts.get(center).x;
                r.bxmin = r.axisboundary = pts.get(center).x;
            }else{
                l.bymax = l.axisboundary = pts.get(center).y;
                r.bymin = r.axisboundary = pts.get(center).y;
            }
            l.medpartition();
            r.medpartition();
            recalcBounds();

        }

        void recalcBounds(){
            xmin = xmax = centerpt.x;
            ymin = ymax = centerpt.y;
            if(l != null){
                xmin = min(l.xmin, xmin);
                ymin = min(l.ymin, ymin);
                xmax = max(l.xmax, xmax);
                ymax = max(l.ymax, ymax);
            }
            if(r != null){
                xmin = min(r.xmin, xmin);
                ymin = min(r.ymin, ymin);
                xmax = max(r.xmax, xmax);
                ymax = max(r.ymax, ymax);
            }
            xcen = (xmin + xmax) / 2;
            ycen = (ymin + ymax) / 2;
        }
    }

    int get(int index, int axis){
        return axis == 0 ? pts.get(index).x : pts.get(index).y;
    }

    void swap(int a, int b){
        pts.swap(a, b);
    }

    //return index of nth smallest after rearranging
    int quickselect(int start, int end, int n, int axis){
        while(true){
            if(end == start){
                return start;
            }
            int pivotIndex = medianPivot(start, end, axis);
            pivotIndex = partition(start, end, pivotIndex, n, axis);
            if(n == pivotIndex){
                return n;
            }else if(n < pivotIndex){
                end = pivotIndex - 1;
            }else{
                start = pivotIndex + 1;
            }
        }
    }

    int medianPivot(int start, int end, int axis){
        if(end - start <= 5){
            insertSort(start, end, axis);
            return (start + end) / 2;
        }
        for(int i = start; i <= end; i += 5){
            int right = i + 4;
            if(right > end){
                right = end;
            }
            insertSort(i, right, axis);
            int med = (i + right) / 2;
            swap(med, start + ((i - start) / 5));
        }
        int mid = (end - start) / 10 + start + 1;
        return quickselect(start, start + ((end - start) / 5), mid, axis);
    }

    int partition(int start, int end, int pivot, int n, int axis){
        int pivotval = get(pivot, axis);
        swap(pivot, end);
        int indexless = start;
        for(int i = start; i < end; i++){
            if(get(i, axis) < pivotval){
                swap(i, indexless);
                indexless++;
            }
        }
        int indexeq = indexless;
        for(int i = indexless; i < end; i++){
            if(get(i, axis) == pivotval){
                swap(i, indexeq);
                indexeq++;
            }
        }
        swap(end, indexeq);
        if(n < indexless){
            return indexless;
        }
        if(n <= indexeq){
            return n;
        }
        return indexeq;
    }

    //end is inclusive
    //sort is ascending
    void insertSort(int start, int end, int axis){
        if(end <= start){
            return;
        }
        //two
        if(end - start == 1){
            if(get(start, axis) > get(end, axis)){
                swap(start, end);
            }
            return;
        }
        //three
        if(end - start == 2){
            int s1 = start + 1;
            boolean e = false;
            if(get(start, axis) > get(s1, axis)){
                swap(start, s1);
                e = true;
            }
            if(get(s1, axis) > get(end, axis)){
                swap(s1, end);
                e = true;
            }
            if(!e){
                return;
            }
            if(get(start, axis) > get(s1, axis)){
                swap(start, s1);
            }
            return;
        }else{
            for(int i = start + 1; i <= end; i++){
                for(int z = i; z > start; z--){
                    if(get(z - 1, axis) > get(z, axis)){
                        swap(z - 1, z);
                    }else{
                        break;
                    }
                }
            }
        }

    }
}
