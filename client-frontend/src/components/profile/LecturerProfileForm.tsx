'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { userService } from '@/services/userService';
import { categoryService } from '@/services/categoryService';
import {
    AcademicInfo,
    CollegeInfo,
    DegreeInfo,
    FacultyInfo,
    LecturerProfileUpdateRequest,
    GenderInfo,
    HierarchicalCategories,
    PositionInfo,
    User
} from '@/types';
import { useToast } from '@/hooks/useToast';
import { useAuth } from '@/contexts/AuthContext';
import UploadFile from '@/components/ui/UploadFile';

interface LecturerProfileFormProps {
    user: User;
}

export default function LecturerProfileForm({ user }: LecturerProfileFormProps) {
    const router = useRouter();
    const { showToast } = useToast();
    const { updateUser } = useAuth();

    const [formData, setFormData] = useState<LecturerProfileUpdateRequest | null>(null);
    const [dropdownData, setDropdownData] = useState({
        hierarchicalData: null as HierarchicalCategories | null,
        colleges: [] as CollegeInfo[],
        faculties: [] as FacultyInfo[],
        genders: [] as GenderInfo[]
    });

    const [filteredFaculties, setFilteredFaculties] = useState<FacultyInfo[]>([]);
    const [selectedCollege, setSelectedCollege] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [dataLoading, setDataLoading] = useState(true);

    const [positionOptions, setPositionOptions] = useState<PositionInfo[]>([]);
    const [academicOptions, setAcademicOptions] = useState<AcademicInfo[]>([]);
    const [degreeOptions, setDegreeOptions] = useState<DegreeInfo[]>([]);

    useEffect(() => {
        const loadDropdownData = async () => {
            try {
                const hierarchicalData = await categoryService.getAllCategories();
                
                // Fix missing codes from backend
                if (hierarchicalData && hierarchicalData.colleges) {
                    hierarchicalData.colleges.forEach(c => {
                        if (!c.code) c.code = c.name;
                        if (c.faculties) {
                            c.faculties.forEach(f => {
                                if (!f.code) f.code = f.name;
                                if (f.majors) {
                                    f.majors.forEach(m => {
                                        if (!m.code) m.code = m.name;
                                    });
                                }
                            });
                        }
                    });
                }

                const colleges = hierarchicalData.colleges.map(college => ({
                    code: college.code,
                    name: college.name,
                }));

                setDropdownData({
                    hierarchicalData,
                    colleges,
                    faculties: [],
                    genders: hierarchicalData.genders,
                });

                setPositionOptions(hierarchicalData.positions || []);
                setAcademicOptions(hierarchicalData.academics || []);
                setDegreeOptions(hierarchicalData.degrees || []);

                if (user.college?.code) {
                    const collegeCode = user.college.code;
                    setSelectedCollege(collegeCode);

                    const selectedCollegeData = hierarchicalData.colleges.find(c => c.code === collegeCode);
                    if (selectedCollegeData) {
                        const facultiesInCollege = selectedCollegeData.faculties.map(faculty => ({
                            code: faculty.code,
                            name: faculty.name,
                            college: { code: collegeCode, name: selectedCollegeData.name },
                        }));

                        setFilteredFaculties(facultiesInCollege);
                    }
                }

                // ✅ Set formData nếu chưa có
                if (!formData) {
                    setFormData({
                        fullName: user.fullName || '',
                        bio: user.bio || '',
                        staffCode: user.staffCode || '',
                        positionCode: user.position?.code || '',
                        academicCode: user.academic?.code || '',
                        degreeCode: user.degree?.code || '',
                        facultyCode: user.faculty?.code || '',
                        genderCode: user.gender?.code || '',
                        avatarUrl: user.avatarUrl || '',
                        backgroundUrl: user.backgroundUrl || ''
                    });
                }
            } catch (error) {
                console.error('Error loading dropdown data:', error);
                showToast('Không thể tải dữ liệu danh mục', 'error');
            } finally {
                setDataLoading(false);
            }
        };

        loadDropdownData();
    }, [user]);

    const handleCollegeChange = (collegeCode: string) => {
        setSelectedCollege(collegeCode);
        setFormData(prev => prev ? { ...prev, collegeCode: collegeCode } : prev);

        if (collegeCode && dropdownData.hierarchicalData) {
            const selectedCollegeData = dropdownData.hierarchicalData.colleges.find(c => c.code === collegeCode);
            if (selectedCollegeData) {
                const facultiesInCollege = selectedCollegeData.faculties.map(faculty => ({
                    name: faculty.name,
                    code: faculty.code,
                    college: { name: selectedCollegeData.name, code: selectedCollegeData.code }
                }));
                setFilteredFaculties(facultiesInCollege);
            }
        } else {
            setFilteredFaculties([]);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (
            !formData?.fullName || !formData.staffCode ||
            !formData.positionCode || !formData.facultyCode || !formData.genderCode
        ) {
            showToast('Vui lòng điền đầy đủ thông tin bắt buộc', 'error');
            return;
        }

        setLoading(true);
        try {
            const updatedUser = await userService.updateMyProfile(formData);
            updateUser(updatedUser);
            showToast('Cập nhật thông tin thành công!', 'success');
            router.replace('/');
        } catch (error) {
            console.error('Error updating profile:', error);
            showToast('Có lỗi xảy ra khi cập nhật thông tin', 'error');
        } finally {
            setLoading(false);
        }
    };

    if (dataLoading || formData === null) {
        return (
            <div className="flex justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-6 text-gray-900">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Full Name */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Họ và tên <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={formData.fullName}
                        onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                        required
                    />
                </div>

                {/* Staff Code */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Mã cán bộ <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={formData.staffCode}
                        onChange={(e) => setFormData({ ...formData, staffCode: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                        required
                    />
                </div>

                {/* Position */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Chức vụ <span className="text-red-500">*</span></label>
                    <select
                        value={formData.positionCode}
                        onChange={(e) => setFormData({ ...formData, positionCode: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                        required
                    >
                        <option value="">Chọn chức vụ</option>
                        {positionOptions.map((option) => (
                            <option key={option.code} value={option.code}>
                                {option.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* College */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Trường <span className="text-red-500">*</span></label>
                    <select
                        value={selectedCollege}
                        onChange={(e) => handleCollegeChange(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                        required
                    >
                        <option value="">Chọn trường</option>
                        {dropdownData.colleges.map((college) => (
                            <option key={college.code} value={college.code}>
                                {college.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Faculty */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Khoa làm việc <span className="text-red-500">*</span></label>
                    <select
                        value={formData.facultyCode}
                        onChange={(e) => setFormData({ ...formData, facultyCode: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                        required
                    >
                        <option value="">Chọn khoa</option>
                        {filteredFaculties.map((faculty) => (
                            <option key={faculty.code} value={faculty.code}>
                                {faculty.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Academic */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Học hàm</label>
                    <select
                        value={formData.academicCode}
                        onChange={(e) => setFormData({ ...formData, academicCode: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    >
                        <option value="">Chọn học hàm</option>
                        {academicOptions.map((option) => (
                            <option key={option.code} value={option.code}>
                                {option.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Degree */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Học vị</label>
                    <select
                        value={formData.degreeCode}
                        onChange={(e) => setFormData({ ...formData, degreeCode: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    >
                        <option value="">Chọn học vị</option>
                        {degreeOptions.map((option) => (
                            <option key={option.code} value={option.code}>
                                {option.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Gender */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Giới tính <span className="text-red-500">*</span></label>
                    <select
                        value={formData.genderCode}
                        onChange={(e) => setFormData({ ...formData, genderCode: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md"
                        required
                    >
                        <option value="">Chọn giới tính</option>
                        {dropdownData.genders.map((gender) => (
                            <option key={gender.code} value={gender.code}>
                                {gender.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* Bio */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Giới thiệu bản thân</label>
                <textarea
                    value={formData.bio}
                    onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="Giới thiệu về bản thân, kinh nghiệm..."
                />
            </div>

            {/* Avatar */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Ảnh đại diện</label>
                <UploadFile
                    currentUser={user}
                    currentImageUrl={formData.avatarUrl}
                    onImageUploaded={(url) => setFormData({ ...formData, avatarUrl: url })}
                    imageType="avatar"
                    aspectRatio="1/1"
                    maxSizeMB={2}
                />
            </div>

            {/* Background */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Ảnh bìa</label>
                <UploadFile
                    currentUser={user}
                    currentImageUrl={formData.backgroundUrl}
                    onImageUploaded={(url) => setFormData({ ...formData, backgroundUrl: url })}
                    imageType="background"
                    aspectRatio="3/1"
                    maxSizeMB={5}
                />
            </div>

            {/* Submit */}
            <div className="flex justify-end space-x-4">
                <button
                    type="button"
                    onClick={() => router.back()}
                    className="px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                >
                    Hủy
                </button>
                <button
                    type="submit"
                    disabled={loading}
                    className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                    {loading ? 'Đang cập nhật...' : 'Cập nhật thông tin'}
                </button>
            </div>
        </form>
    );
}
