import {z} from "zod";
import {useParams} from "react-router-dom";
import {useMutation, useQueryClient} from "@tanstack/react-query";
import {Controller, useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";

import {AuthenticatedAxios} from "@/services/http/Axios";

import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card";
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {Label} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {Button} from "@/components/ui/button";
import {ButtonLoading} from "@/components/common/ButtonLoading";
import {toastify} from "@/common/toastify.js";
import {TOAST_TYPE} from "@/constants/app.constants";
import {handleErrors} from "@/utils";
import {queryKeys} from "@/services/reactQuery/queryKeys";
import {useUserStore} from "@/store/useUserStore";

const ReviewSchema = z.object({
    rating: z.number().int().min(1).max(5),
    comment: z.string().optional(),
});

const saveReview = async (id, data) => {
    const response = await AuthenticatedAxios.post(`/products/${id}/review`, data);
    return response.data.data;
}

const ReviewSave = () => {
    const {id} = useParams();
    const {user} = useUserStore();
    const queryClient = useQueryClient();

    const {
        register,
        control,
        handleSubmit,
        reset,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(ReviewSchema),
        defaultValues: {
            rating: 5,
            comment: "",
        },
    });

    const createReviewMutation = useMutation({
        mutationFn: (data) => saveReview(id, data),

        onMutate: async (newReview) => {
            await queryClient.cancelQueries({queryKey: queryKeys.reviews.all(id)});

            const previousReviews = queryClient.getQueryData(queryKeys.reviews.all(id));

            queryClient.setQueryData(queryKeys.reviews.all(id), (old) => {
                const optimisticReview = {
                    id: Date.now(),
                    ...newReview,
                    createdAt: new Date().toISOString(),
                    user: {name: user?.name}
                };
                return old ? [...old, optimisticReview] : [optimisticReview];
            });

            return {previousReviews};
        },
        onSuccess: () => {
            toastify(TOAST_TYPE.SUCCESS, "Review added successfully.");
            reset();
        },
        onError: (err, newReview, context) => {
            if (context?.previousReviews) {
                queryClient.setQueryData(queryKeys.reviews.all(id), context.previousReviews);
            }
            console.error(err);
            handleErrors(err, setError);
        },
        onSettled: () => {
            // Always refetch after error or success to sync with server
            queryClient.invalidateQueries({queryKey: queryKeys.reviews.all(id)});
        },
    });

    const onSubmit = (data) => {
        createReviewMutation.mutate(data);
    };

    return (
        <Card className="h-fit">
            <form onSubmit={handleSubmit(onSubmit)}>
                <CardHeader>
                    <CardTitle>Add Review</CardTitle>
                </CardHeader>

                <CardContent className="space-y-4 px-6">
                    <div className="space-y-2">
                        <Label className="flex">
                            Rating
                            <span className="text-red-600">*</span>
                        </Label>

                        <Controller
                            name="rating"
                            control={control}
                            render={({field}) => (
                                <Select
                                    value={String(field.value)}
                                    onValueChange={(value) =>
                                        field.onChange(Number(value))
                                    }
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select Rating"/>
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectGroup>
                                            {[1, 2, 3, 4, 5].map((num) => (
                                                <SelectItem
                                                    key={num}
                                                    value={String(num)}
                                                >
                                                    {num} Star
                                                    {num > 1 && "s"}
                                                </SelectItem>
                                            ))}
                                        </SelectGroup>
                                    </SelectContent>
                                </Select>
                            )}
                        />

                        <p className="validation-error">
                            {errors.rating?.message}
                        </p>
                    </div>

                    <div className="space-y-2">
                        <Label className="flex">
                            Review
                            <span className="text-red-600">*</span>
                        </Label>

                        <Textarea
                            {...register("comment")}
                            placeholder="Write your review..."
                        />

                        <p className="validation-error">
                            {errors.comment?.message}
                        </p>
                    </div>
                </CardContent>

                <CardFooter className="flex-col gap-2">
                    {isSubmitting ? (
                        <ButtonLoading/>
                    ) : (
                        <Button
                            type="submit"
                            className="w-full bg-blue-600"
                        >
                            Add Review
                        </Button>
                    )}
                </CardFooter>
            </form>
        </Card>
    );
};

export default ReviewSave;

