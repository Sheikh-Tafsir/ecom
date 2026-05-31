import React from 'react'

const InputViewMode = ({ value, isEditable: isInEditMode }) => {
  return (
    <p className={`mb-[4%] border rounded-lg px-4 ${(value || value == 0) ? "py-2" : "py-5 "} 
        ${isInEditMode ? "text-gray-400 border-gray-200" : "text-gray-400 border-gray-100"}`}>
      {value}
    </p>
  );
};

export default InputViewMode