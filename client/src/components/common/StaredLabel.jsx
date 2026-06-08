import { Label } from '@/components/ui/label'

const StaredLabel = ({label, field= null}) => {
  return (
    <Label htmlFor={field ?? label.toLowerCase()} className="flex mb-2">{label}<p className="text-red-600">*</p></Label>
  )
}

export default StaredLabel