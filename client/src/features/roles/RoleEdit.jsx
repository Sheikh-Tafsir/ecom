import React, {useEffect} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';
import {useForm, Controller} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQuery, useMutation, useQueryClient} from '@tanstack/react-query';
import {z} from 'zod';

import {Button} from '@/components/ui/button.jsx';
import {
    Card,
    CardContent,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.jsx';
import {Input} from '@/components/ui/input.jsx';
import {Axios} from '@/services/http/Axios';
import {ButtonLoading} from "@/components/common/ButtonLoading";
import PageLoadingOverlay from '@/components/common/pageLoadingOverlay/PageLoadingOverlay';
import StaredLabel from '@/components/common/StaredLabel';
import {TOAST_TYPE, PERMISSION} from '@/utils/enums';
import {GLOBAL_ERROR, handleErrors} from '@/utils/ErrorUtils';
import InputError from "@/components/common/InputError.jsx";
import {notify} from '@/components/common/notification';
import {Checkbox} from "@/components/ui/checkbox";
import {Label} from "@/components/ui/label";

const RoleSchema = z.object({
    name: z.string().min(2, "Role name is required"),
    permissions: z.array(z.string()).min(1, "At least one permission is required"),
});

const RoleEdit = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const queryClient = useQueryClient();
    
    const roleFromState = location.state?.role;

    const {register, handleSubmit, control, reset, setError, formState: {errors, isSubmitting}} = useForm({
        resolver: zodResolver(RoleSchema),
        defaultValues: {
            name: '',
            permissions: []
        },
    });

    const {data: roleFromQuery, isLoading} = useQuery({
        queryKey: ["role", id],
        queryFn: async () => {
            const res = await Axios.get(`/roles/${id}`);
            return res.data.data;
        },
        enabled: !!id && !roleFromState,
    });

    useEffect(() => {
        const role = roleFromState || roleFromQuery;
        if (role) {
            reset({
                name: role.name.replace('ROLE_', ''),
                permissions: role.permissions
            });
        }
    }, [reset, roleFromState, roleFromQuery]);

    const mutation = useMutation({
        mutationFn: async (data) => {
            if (id) {
                await Axios.put(`/roles/${id}`, data);
            } else {
                await Axios.post('/roles', data);
            }
        },
        onSuccess: () => {
            notify(TOAST_TYPE.SUCCESS, `Role successfully ${id ? 'updated' : 'created'}`);
            queryClient.invalidateQueries({queryKey: ["roles"]});
            navigate('/roles');
        },
        onError: (error) => {
            handleErrors(error, setError);
            notify(TOAST_TYPE.ERROR, "Failed to save role");
        },
    });

    if (isLoading) return <PageLoadingOverlay/>;

    return (
        <div className="min-h-[90vh] flex items-center justify-center p-4">
            <Card className="w-full max-w-md">
                <form onSubmit={handleSubmit((data) => mutation.mutate(data))}>
                    <CardHeader>
                        <CardTitle>{id ? 'Edit Role' : 'Create New Role'}</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <InputError errors={errors} field={GLOBAL_ERROR}/>
                        
                        <div className="space-y-2">
                            <StaredLabel label="Role Name"/>
                            <Input {...register("name")} placeholder="e.g. MANAGER"/>
                            {errors.name && <p className="text-sm text-red-500">{errors.name.message}</p>}
                        </div>

                        <div className="space-y-3">
                            <StaredLabel label="Permissions"/>
                            <div className="grid grid-cols-1 gap-2">
                                {Object.entries(PERMISSION).map(([key, value]) => (
                                    <div key={value} className="flex items-center space-x-2">
                                        <Controller
                                            name="permissions"
                                            control={control}
                                            render={({field}) => (
                                                <Checkbox
                                                    id={value}
                                                    checked={field.value?.includes(value)}
                                                    onCheckedChange={(checked) => {
                                                        const newValue = checked
                                                            ? [...(field.value || []), value]
                                                            : field.value?.filter((v) => v !== value);
                                                        field.onChange(newValue);
                                                    }}
                                                />
                                            )}
                                        />
                                        <Label htmlFor={value} className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                                            {key} ({value})
                                        </Label>
                                    </div>
                                ))}
                            </div>
                            {errors.permissions && <p className="text-sm text-red-500">{errors.permissions.message}</p>}
                        </div>
                    </CardContent>
                    <CardFooter className="flex justify-end gap-2">
                        <Button type="button" variant="outline" onClick={() => navigate('/roles')}>
                            Cancel
                        </Button>
                        {isSubmitting ? <ButtonLoading /> : (
                            <Button type="submit" className="bg-blue-600">
                                {id ? 'Save Changes' : 'Create Role'}
                            </Button>
                        )}
                    </CardFooter>
                </form>
            </Card>
        </div>
    );
};

export default RoleEdit;
