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
import {Label} from "@/components/ui/label";
import { MultiSelect } from '@/components/common/MultiSelect';

const RoleSchema = z.object({
    name: z.string().min(2, "Role name is required"),
    permissions: z.array(z.string()).min(1, "At least one permission is required"),
});

const fetchRole = async (id) => {
    const response = await Axios.get(`/roles/${id}`)
    return response.data.data
};

const RoleSave = () => {
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
        queryFn: () => fetchRole(id),
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
            const payload = {
                ...data,
            };
            if (id) {
                await Axios.put(`/roles/${id}`, payload);
            } else {
                await Axios.post('/roles', payload);
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
        <div className="min-h-[85vh] flex items-center justify-center">
            <Card className="w-full max-w-lg border-slate-100 shadow-xl shadow-slate-200/50 rounded-lg overflow-hidden">
                <form onSubmit={handleSubmit((data) => mutation.mutate(data))}>
                    <CardHeader className="bg-slate-100 border-b border-slate-100 pb-6">
                        <CardTitle className="text-2xl font-bold text-slate-800 tracking-tight">
                            {id ? 'Edit Role' : 'Create New Role'}
                        </CardTitle>
                        <p className="text-slate-500 font-medium text-sm">
                            {id ? 'Update existing role permissions and name' : 'Define a new role and assign its access levels'}
                        </p>
                    </CardHeader>
                    
                    <CardContent className="space-y-6 pt-8 px-8">
                        <InputError errors={errors} field={GLOBAL_ERROR}/>

                        <div className="space-y-3">
                            <Label className="text-sm font-bold uppercase tracking-widest text-slate-600">
                                <StaredLabel label="Role Name"/>
                            </Label>
                            <div className="flex items-center gap-2">
                                <span className="bg-slate-100 px-3 h-11 flex items-center rounded-lg font-bold text-slate-400 border border-slate-200">ROLE_</span>
                                <Controller
                                    name="name"
                                    control={control}
                                    render={({ field }) => (
                                        <Input
                                            {...field}
                                            placeholder="e.g. MANAGER"
                                            value={field.value}
                                            onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                                            className="h-11 rounded-lg border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all font-bold text-slate-700"
                                        />
                                    )}
                                />
                            </div>
                            <InputError errors={errors} field="name"/>
                        </div>

                        <div className="space-y-3">
                            <Label className="text-sm font-bold uppercase tracking-widest text-slate-600">
                                <StaredLabel label="Permissions"/>
                            </Label>
                            <Controller
                                name="permissions"
                                control={control}
                                render={({field}) => (
                                    <MultiSelect
                                        options={Object.entries(PERMISSION).map(([key, value]) => ({
                                            label: key.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' '),
                                            value: value,
                                        }))}
                                        selected={field.value}
                                        onChange={field.onChange}
                                        placeholder="Select permissions..."
                                        className="border-slate-200 focus:ring-4 focus:ring-blue-50 focus:border-blue-400 transition-all"
                                    />
                                )}
                            />
                            <InputError errors={errors} field="permissions"/>
                        </div>
                    </CardContent>
                    
                    <CardFooter className="flex justify-end gap-3 px-8 pb-8 pt-4">
                        <Button 
                            type="button" 
                            variant="ghost" 
                            onClick={() => navigate('/roles')}
                            className="font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-100 h-12 px-6 rounded-lg"
                        >
                            Cancel
                        </Button>
                        {isSubmitting ? (
                            <ButtonLoading className="h-12 px-8 rounded-lg bg-blue-600 w-[140px]" />
                        ) : (
                            <Button 
                                type="submit" 
                                className="bg-blue-600 hover:bg-blue-700 text-white font-bold h-12 px-8 rounded-lg shadow-lg shadow-blue-200 transition-all active:scale-95"
                            >
                                {id ? 'Save Changes' : 'Create Role'}
                            </Button>
                        )}
                    </CardFooter>
                </form>
            </Card>
        </div>
    );
};

export default RoleSave;
