# Time Complexities of Common Sorting Algorithms

| Algorithm      | Best case   | Average case | Worst case  | Space Complexity | Stable? |
|----------------|-------------|--------------|-------------|------------------|---------|
| **Bubble Sort**| $O(n)$      | $O(n^2)$     | $O(n^2)$    | $O(1)$           | Yes     |
| **Insertion Sort**| $O(n)$   | $O(n^2)$     | $O(n^2)$    | $O(1)$           | Yes     |
| **Selection Sort**| $O(n^2)$ | $O(n^2)$     | $O(n^2)$    | $O(1)$           | No      |
| **Merge Sort** | $O(n \log n)$| $O(n \log n)$| $O(n \log n)$| $O(n)$         | Yes     |
| **Quick Sort** | $O(n \log n)$| $O(n \log n)$| $O(n^2)$    | $O(\log n)$      | No      |
| **Heap Sort**  | $O(n \log n)$| $O(n \log n)$| $O(n \log n)$| $O(1)$         | No      |

## Merge Sort (Current Context)
Merge Sort is a divide-and-conquer algorithm. It always divides the array into two halves, recursively sorts them, and then merges the sorted halves.

- **Time Complexity**: $O(n \log n)$ in all cases because the recurrence relation is always $T(n) = 2T(n/2) + O(n)$.
- **Space Complexity**: $O(n)$ because of the temporary arrays used during the merge step.

