export const priorityList = ["high", "medium", "low"];

export const getPriorityColor = (priority) => {
    switch (priority) {
      case priorityList[0]:
        return 'bg-red-500 hover:bg-red-600'
      case priorityList[1]:
        return 'bg-yellow-500 hover:bg-yellow-600'
      case priorityList[3]:
        return 'bg-green-500 hover:bg-green-600'
      default:
        return 'bg-gray-500 hover:bg-gray-600'
    }
}