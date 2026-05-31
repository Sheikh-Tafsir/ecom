import { Label } from '@/components/ui/label'
import React from 'react'

const StaredLabel = ({label}) => {
  return (
    <Label className="flex mb-2">{label}<p className="text-red-600">*</p></Label>
  )
}

export default StaredLabel